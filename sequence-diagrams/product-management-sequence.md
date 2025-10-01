# Biểu đồ trình tự - Use case: Quản lý sản phẩm (Product Management)

## Mô tả
Use case này cho phép Admin quản lý các sản phẩm trong hệ thống, bao gồm: xem danh sách, tìm kiếm, phân trang, thêm mới, chỉnh sửa và xóa sản phẩm. Hệ thống hỗ trợ tìm kiếm theo ID cụ thể, lọc theo category, upload hình ảnh và pagination để xử lý large datasets.

## 1. Sequence Diagram - Khởi tạo và Load danh sách sản phẩm

```plantuml
@startuml
title Product Management - Load danh sách sản phẩm với phân trang

actor Admin as A
boundary "ProductManagementUI" as UI
control "ProductController" as PC
entity "Product" as P
entity "Category" as C
database MySQL as DB

== Authentication và khởi tạo ==
A -> UI: Truy cập trang Product Management
UI -> UI: Kiểm tra authentication và role
note over UI: Chỉ hiển thị CRUD buttons\nnếu user có ADMIN role

== Khởi tạo component state ==
UI -> UI: Initialize component properties
note over UI: products: []\nallProducts: []\ncurrentPage: 1\ntotalPages: 0\nitemsPerPage: 10\nselectedCategory: ''

== Load products với query params ==
UI -> UI: Listen to route query params
note over UI: Query params: ?category=categoryName

alt Có selectedCategory từ query params
    UI -> PC: getAllProductsByCategoryName(categoryName, searchText)
    PC -> C: findByName(categoryName)
    C -> DB: SELECT * FROM categories WHERE name = ?
    
    alt Category tồn tại
        DB -> C: Category entity
        C -> PC: Category found
        PC -> P: findAllByCategory_Name(categoryName, searchText, Sort.DESC)
        P -> DB: Complex query với JOIN
        note over DB: SELECT p.* FROM products p\nLEFT JOIN categories c ON p.category_id = c.id\nWHERE c.name = ? AND (\n  searchText IS NULL OR\n  LOWER(p.name) LIKE LOWER(CONCAT('%', searchText, '%')) OR\n  LOWER(p.sku) LIKE LOWER(CONCAT('%', searchText, '%'))\n) ORDER BY p.id DESC
        DB -> P: List of products by category
        P -> PC: List<Product>
        PC -> UI: Response{category: CategoryDTO, products: List<ProductDTO>}
        UI -> UI: Pagination processing
        note over UI: totalPages = Math.ceil(allProducts.length / itemsPerPage)\nproducts = allProducts.slice(startIndex, endIndex)
        UI -> A: Hiển thị products với category filter và pagination
        
    else Category không tồn tại
        DB -> C: null
        C -> PC: NotFoundException("Category Not Found")
        PC -> UI: Error response
        UI -> A: Hiển thị lỗi "Category not found"
    end
    
else Không có category filter (all products)
    UI -> PC: getAllProducts(searchText)
    PC -> P: findAll(searchText, Sort.DESC)
    P -> DB: Query với search conditions
    note over DB: SELECT * FROM products WHERE (\n  searchText IS NULL OR\n  LOWER(name) LIKE LOWER(CONCAT('%', searchText, '%')) OR\n  LOWER(sku) LIKE LOWER(CONCAT('%', searchText, '%'))\n) ORDER BY id DESC
    DB -> P: List of all products
    P -> PC: List<Product>
    PC -> UI: Response{status: 200, products: List<ProductDTO>}
    UI -> UI: Client-side pagination processing
    UI -> A: Hiển thị all products với pagination
end

@enduml
```

## 2. Sequence Diagram - Tìm kiếm sản phẩm theo ID

```plantuml
@startuml
title Product Search by ID - Tìm kiếm sản phẩm theo ID

actor Admin as A
boundary "ProductManagementUI" as UI
control "ProductController" as PC
entity "Product" as P
database MySQL as DB

== Tìm kiếm sản phẩm cụ thể ==
A -> UI: Nhập product ID vào search box
A -> UI: Click "Go!" button

UI -> UI: Validate search input
alt Valid input (không empty)
    UI -> UI: Set isSearching = true
    UI -> UI: Set currentPage = 1
    UI -> PC: getProductById(productId)
    PC -> P: findById(productId)
    P -> DB: SELECT * FROM products WHERE id = ?
    
    alt Product tồn tại
        DB -> P: Product entity
        P -> PC: Product found
        PC -> UI: Response{status: 200, product: ProductDTO}
        UI -> UI: Set search result
        note over UI: products = [foundProduct]\nallProducts = [foundProduct]\ntotalPages = 1
        UI -> A: Hiển thị single product found với success notification
        
    else Product không tồn tại
        DB -> P: null
        P -> PC: NotFoundException("Product Not Found")
        PC -> UI: Error response
        UI -> UI: Clear results
        note over UI: products = []\nallProducts = []\ntotalPages = 0
        UI -> A: Hiển thị "Product not found" error
    end
    
else Invalid input (empty search)
    UI -> A: Hiển thị "Please enter a product ID" error
end

== Cancel search và quay về full list ==
A -> UI: Click "Cancel" button
UI -> UI: Reset search state
note over UI: searchInput = ''\nvalueToSearch = ''\nisSearching = false\ncurrentPage = 1
UI -> UI: fetchProducts() - reload full list
UI -> A: Quay về hiển thị tất cả products

@enduml
```

## 3. Sequence Diagram - Thêm sản phẩm mới

```plantuml
@startuml
title Add New Product - Thêm sản phẩm mới

actor Admin as A
boundary "AddEditProductUI" as UI
control "ProductController" as PC
entity "Product" as P
entity "Category" as C
database MySQL as DB
control "FileSystemController" as FS

== Navigate to Add Product ==
A -> UI: Click "Add Product" button từ Product List
UI -> UI: navigateToAddProductPage()
UI -> A: Chuyển đến trang /add-product

== Initialize Add Product Form ==
UI -> UI: ngOnInit() - Initialize form
UI -> UI: fetchCategories()
UI -> PC: getAllCategories()
PC -> C: findAll()
C -> DB: SELECT * FROM categories
DB -> C: List of categories
C -> PC: List<Category>
PC -> UI: Response{categories: List<CategoryDTO>}
UI -> UI: Populate category dropdown
UI -> A: Hiển thị form thêm product với category options

== Fill Product Information ==
A -> UI: Nhập thông tin product
note over A: name, sku, price, stockQuantity,\ncategoryId, description
A -> UI: Select image file
UI -> UI: handleImageChange(event)
UI -> UI: Create FileReader và preview image
UI -> A: Hiển thị image preview

== Submit New Product ==
A -> UI: Click "Submit" button
UI -> UI: handleSubmit(event) - Create FormData
note over UI: FormData contains:\n- name, sku, price, stockQuantity\n- categoryId, description\n- imageFile (if selected)

UI -> PC: saveProduct(FormData)
PC -> C: findById(categoryId)
C -> DB: SELECT * FROM categories WHERE id = ?

alt Category tồn tại
    DB -> C: Category entity
    C -> PC: Valid category
    
    alt Image file được provide
        PC -> FS: saveImageToFrontendPublicFolder(imageFile)
        FS -> FS: Validate image file (type, size)
        FS -> FS: Generate unique filename
        note over FS: filename = timestamp + "_" + randomNumber + "_" + originalName
        FS -> FS: Save to frontend/public/images/
        FS -> PC: Return image URL path
    end
    
    PC -> P: Build Product entity
    note over PC: Product.builder()\n.name(name).sku(sku)\n.price(price).stockQuantity(stockQuantity)\n.category(category).description(description)\n.imageUrl(imageUrl).build()
    
    PC -> P: save(newProduct)
    P -> DB: INSERT INTO products (name, sku, price, stock_quantity, category_id, description, image_url, created_at) VALUES (...)
    DB -> P: Saved product với auto-generated ID
    P -> PC: Product entity
    PC -> UI: Response{status: 200, message: "Product successfully saved"}
    UI -> UI: Show success notification
    UI -> UI: navigate(['/product'])
    UI -> A: Chuyển về Product List với thông báo thành công
    
else Category không tồn tại
    DB -> C: null
    C -> PC: NotFoundException("Category Not Found")
    PC -> UI: Error response
    UI -> A: Hiển thị "Category not found" error
end

== Handle Validation Errors ==
alt Required fields missing (name, sku, price, stockQuantity, categoryId)
    UI -> A: Form validation error - required fields
    
else Invalid file type/size
    FS -> PC: File validation error
    PC -> UI: Invalid image file error
    UI -> A: Hiển thị "Invalid image file" error
    
else Duplicate SKU
    P -> DB: INSERT fails due to unique constraint
    DB -> P: Unique constraint violation on SKU
    P -> PC: Database constraint error
    PC -> UI: SKU already exists error
    UI -> A: Hiển thị "SKU already exists" error
end

@enduml
```

## 4. Sequence Diagram - Chỉnh sửa sản phẩm

```plantuml
@startuml
title Edit Product - Chỉnh sửa sản phẩm

actor Admin as A
boundary "AddEditProductUI" as UI
control "ProductController" as PC
entity "Product" as P
entity "Category" as C
database MySQL as DB
control "FileSystemController" as FS

== Navigate to Edit Product ==
A -> UI: Click "Edit" button trên product trong list
UI -> UI: navigateToEditProductPage(productId)
UI -> A: Chuyển đến trang /edit-product/{id}

== Initialize Edit Form ==
UI -> UI: ngOnInit() - Detect productId từ route params
UI -> UI: Set isEditing = true
UI -> UI: fetchCategories() - Load categories for dropdown
UI -> UI: fetchProductById(productId)

UI -> PC: getProductById(productId)
PC -> P: findById(productId)
P -> DB: SELECT * FROM products WHERE id = ?

alt Product tồn tại
    DB -> P: Product entity với category relationship
    P -> PC: Product with Category
    PC -> UI: Response{status: 200, product: ProductDTO}
    UI -> UI: Pre-fill form với existing data
    note over UI: name = product.name\nsku = product.sku\nprice = product.price\nstockQuantity = product.stockQuantity\ncategoryId = product.categoryId\ndescription = product.description\nimageUrl = product.imageUrl
    UI -> A: Form hiển thị với dữ liệu current product
    
else Product không tồn tại
    DB -> P: null
    P -> PC: NotFoundException("Product Not Found")
    PC -> UI: Error response
    UI -> A: Hiển thị "Product not found" và redirect về list
end

== Update Product Information ==
A -> UI: Modify product information
A -> UI: Optionally change image
alt New image selected
    UI -> UI: handleImageChange(event)
    UI -> UI: Update imageFile và imageUrl preview
    UI -> A: Hiển thị new image preview
end

== Submit Product Update ==
A -> UI: Click "Submit" button
UI -> UI: handleSubmit(event) - Create FormData with productId
UI -> PC: updateProduct(FormData)
PC -> P: findById(productId)
P -> DB: SELECT * FROM products WHERE id = ?

alt Product tồn tại
    DB -> P: Existing product entity
    P -> PC: Product found
    
    alt New image provided
        PC -> FS: saveImageToFrontendPublicFolder(newImageFile)
        FS -> FS: Save new image và return URL
        FS -> PC: New image URL
        PC -> P: Set imageUrl = newImageUrl
    end
    
    alt Category changed
        PC -> C: findById(newCategoryId)
        C -> DB: SELECT * FROM categories WHERE id = ?
        DB -> C: New category entity
        C -> PC: Valid category
        PC -> P: Set category = newCategory
    end
    
    PC -> P: Conditional field updates
    note over PC: if (name != null && !name.isBlank()) setName(name)\nif (sku != null && !sku.isBlank()) setSku(sku)\nif (description != null) setDescription(description)\nif (price != null && price >= 0) setPrice(price)\nif (stockQuantity != null && stockQuantity >= 0) setStockQuantity(stockQuantity)
    
    PC -> P: save(updatedProduct)
    P -> DB: UPDATE products SET name=?, sku=?, price=?, stock_quantity=?, category_id=?, description=?, image_url=?, updated_at=? WHERE id=?
    DB -> P: Updated product
    P -> PC: Product updated
    PC -> UI: Response{status: 200, message: "Product successfully Updated"}
    UI -> UI: Show success notification
    UI -> UI: navigate(['/product'])
    UI -> A: Quay về Product List với thông báo cập nhật thành công
    
else Product không tồn tại
    DB -> P: null
    P -> PC: NotFoundException("Product Not Found")
    PC -> UI: Error response
    UI -> A: Hiển thị "Product not found" error
end

== Handle Update Errors ==
alt Duplicate SKU (khi update SKU)
    P -> DB: UPDATE fails due to unique constraint
    DB -> P: Unique constraint violation
    P -> PC: SKU already exists error
    PC -> UI: Error response
    UI -> A: Hiển thị "SKU already exists" error
    
else Invalid category
    C -> DB: SELECT returns null
    DB -> C: Category not found
    C -> PC: NotFoundException("Category Not Found")
    PC -> UI: Error response
    UI -> A: Hiển thị "Category not found" error
    
else File upload error
    FS -> PC: File processing error
    PC -> UI: File error response
    UI -> A: Hiển thị file upload error
end

@enduml
```

## 5. Sequence Diagram - Xóa sản phẩm

```plantuml
@startuml
title Delete Product - Xóa sản phẩm

actor Admin as A
boundary "ProductManagementUI" as UI
control "ProductController" as PC
entity "Product" as P
database MySQL as DB

== Delete Product Flow ==
A -> UI: Click "Delete" button trên product
UI -> UI: Show confirmation dialog
UI -> A: "Are you sure you want to delete this product?"

alt User confirms deletion
    A -> UI: Click "OK" trong confirmation dialog
    UI -> PC: deleteProduct(productId)
    PC -> P: findById(productId)
    P -> DB: SELECT * FROM products WHERE id = ?
    
    alt Product tồn tại
        DB -> P: Product entity
        P -> PC: Product found
        
        == Check foreign key constraints ==
        PC -> DB: Check if product được reference trong transactions
        note over DB: Check foreign key references:\n- transaction_items table\n- purchase_orders table\n- sales_records table
        
        alt Không có foreign key constraints
            PC -> P: deleteById(productId)
            P -> DB: DELETE FROM products WHERE id = ?
            DB -> P: Delete successful
            P -> PC: Deletion confirmed
            PC -> UI: Response{status: 200, message: "Product successfully deleted"}
            UI -> UI: Show success notification
            UI -> UI: fetchProducts() - Reload product list
            UI -> A: Product biến mất khỏi list với success notification
            
        else Product đang được sử dụng trong transactions
            DB -> PC: Foreign key constraint violation
            PC -> UI: Error response với constraint message
            UI -> A: Hiển thị "Cannot delete product in use" error
        end
        
    else Product không tồn tại
        DB -> P: null
        P -> PC: NotFoundException("Product Not Found")
        PC -> UI: Error response
        UI -> A: Hiển thị "Product not found" error
    end
    
else User cancels deletion
    A -> UI: Click "Cancel" trong confirmation dialog
    UI -> A: Không làm gì, quay về list
end

@enduml
```

## 6. Sequence Diagram - Pagination và Navigation

```plantuml
@startuml
title Product Pagination và Navigation

actor Admin as A
boundary "ProductManagementUI" as UI
boundary "CustomPaginationComponent" as PG
control "ProductController" as PC
entity "Product" as P
database MySQL as DB

== Pagination Initialization ==
UI -> UI: fetchProducts() completion
UI -> UI: Calculate pagination
note over UI: totalPages = Math.ceil(allProducts.length / itemsPerPage)\nproducts = allProducts.slice((currentPage-1) * itemsPerPage, currentPage * itemsPerPage)
UI -> PG: Pass pagination props
note over PG: [currentPage]="currentPage"\n[totalPages]="totalPages"\n(pageChange)="onPageChange($event)"
PG -> A: Hiển thị pagination controls

== Page Navigation ==
A -> PG: Click page number hoặc next/previous
PG -> UI: Emit pageChange event với new page number
UI -> UI: onPageChange(page)
UI -> UI: Set currentPage = page

alt Not searching by ID (normal pagination)
    UI -> UI: fetchProducts() - Reload data cho new page
    UI -> PC: API call với current filters
    PC -> P: Database query
    P -> DB: Query với pagination parameters
    DB -> P: Results for new page
    P -> PC: Paginated results
    PC -> UI: Response với new page data
    UI -> UI: Update products array cho new page
    UI -> A: Hiển thị new page results
    
else Searching by ID (single result)
    UI -> UI: Skip fetchProducts() - keep single result
    note over UI: Không reload khi search by ID\nvì chỉ có 1 result
    UI -> A: Maintain single search result
end

== Category Filter Navigation ==
A -> UI: Select category từ navigation hoặc URL
UI -> UI: Route query params change
UI -> UI: Listen to queryParams.subscribe()
UI -> UI: Update selectedCategory
UI -> UI: Reset currentPage = 1
UI -> UI: fetchProducts() with new category filter
UI -> A: Hiển thị filtered results với pagination reset

== Search and Filter Interaction ==
A -> UI: Perform search while có category filter
UI -> UI: Combine search + category filter
UI -> PC: getAllProductsByCategoryName(category, searchText)
PC -> P: findAllByCategory_Name với search conditions
P -> DB: Complex query với category JOIN và search conditions
DB -> P: Filtered và searched results
P -> PC: Combined filter results
PC -> UI: Response với filtered data
UI -> UI: Apply client-side pagination to filtered results
UI -> A: Hiển thị searched results trong category với pagination

@enduml
```

## 7. Sequence Diagram - Error Handling và Edge Cases

```plantuml
@startuml
title Product Management - Error Handling và Edge Cases

actor Admin as A
boundary "ProductManagementUI" as UI
control "ProductController" as PC
entity "Product" as P
database MySQL as DB
control "FileSystemController" as FS

== Network và Authentication Errors ==
A -> UI: Perform any product operation
UI -> PC: API call với authentication token

alt Token expired hoặc invalid
    PC -> UI: 401/403 Unauthorized response
    UI -> UI: Clear stored authentication
    UI -> A: Redirect to login page với "Session expired" message
    
else Network connection error
    UI -> PC: Request timeout hoặc network failure
    PC -> UI: Network error
    UI -> A: Hiển thị "Unable to connect to server" error
    
else Server internal error
    PC -> DB: Database operation fails
    DB -> PC: 500 Internal Server Error
    PC -> UI: Server error response
    UI -> A: Hiển thị "Server error occurred, please try again" message
end

== File Upload Specific Errors ==
A -> UI: Upload invalid image file
UI -> UI: handleImageChange(event)
UI -> FS: Validate file type và size

alt Invalid file type (not image)
    FS -> UI: File type validation error
    UI -> A: Hiển thị "Please select a valid image file (JPG, PNG, GIF)" error
    
else File size too large
    FS -> UI: File size validation error
    UI -> A: Hiển thị "File size must be less than 5MB" error
    
else File system permission error
    FS -> PC: Unable to save file to directory
    PC -> UI: File system error
    UI -> A: Hiển thị "Unable to save image file" error
end

== Data Validation Errors ==
A -> UI: Submit form với invalid data
UI -> UI: Form validation

alt Client-side validation fails
    UI -> A: Form field errors
    note over A: Required field highlighting\nInvalid format messages
    
else Server-side validation fails
    UI -> PC: API call với invalid data
    PC -> P: Entity validation
    
    alt SKU already exists
        P -> DB: Unique constraint violation on sku column
        DB -> P: Constraint violation error
        P -> PC: Duplicate SKU error
        PC -> UI: 400 Bad Request với specific message
        UI -> A: Hiển thị "SKU already exists" error
        
    else Negative price hoặc stock
        P -> PC: @Positive/@Min validation fails
        PC -> UI: Validation error response
        UI -> A: Hiển thị "Price must be positive" error
        
    else Invalid category reference
        PC -> P: Category not found during save
        P -> PC: Foreign key constraint error
        PC -> UI: Invalid category error
        UI -> A: Hiển thị "Selected category no longer exists" error
    end
end

== Concurrent Access Issues ==
A -> UI: Edit product đã được modified bởi user khác
UI -> PC: updateProduct(productData)
PC -> P: findById(productId)

alt Product đã được deleted bởi user khác
    P -> DB: Product no longer exists
    DB -> P: null result
    P -> PC: NotFoundException
    PC -> UI: Product not found error
    UI -> A: Hiển thị "Product has been deleted by another user" và redirect về list
    
else Product data đã outdated (optimistic locking)
    P -> DB: Version/timestamp mismatch
    DB -> P: Optimistic lock exception
    P -> PC: Concurrent modification error
    PC -> UI: Stale data error
    UI -> A: Hiển thị "Product has been modified, please refresh and try again"
end

== Pagination Edge Cases ==
UI -> UI: Calculate pagination với no results

alt Empty product list
    UI -> UI: products.length === 0
    UI -> A: Hiển thị "No products found" message
    UI -> PG: Hide pagination controls
    
else Invalid page number (URL manipulation)
    A -> UI: Navigate to ?page=999 (page không tồn tại)
    UI -> UI: Detect currentPage > totalPages
    UI -> UI: Reset currentPage = 1
    UI -> UI: fetchProducts() để correct page
    UI -> A: Redirect về page 1 với valid results
    
else Page size too large
    UI -> UI: itemsPerPage causes performance issues
    UI -> UI: Limit itemsPerPage = Math.min(itemsPerPage, 100)
    UI -> A: Apply reasonable pagination limits
end

== Search Edge Cases ==
A -> UI: Search với special characters hoặc SQL injection attempts
UI -> PC: getProductById(maliciousInput)
PC -> P: Prepared statement protection
P -> DB: Safe parameterized query
DB -> P: No results hoặc safe execution
P -> PC: Safe response
PC -> UI: No results found
UI -> A: "Product not found" - không expose security info

== Recovery Actions ==
UI -> UI: Implement retry mechanisms
note over UI: Retry failed API calls với exponential backoff\nCache results để reduce server load\nGraceful degradation khi features fail

@enduml
```

## Đặc điểm nổi bật của Product Management System

### 1. **Complex Search và Filtering**
```typescript
interface ProductSearchParams {
    searchText?: string;        // Search by name, SKU, stock quantity
    categoryName?: string;      // Filter by category
    page?: number;             // Pagination
    size?: number;             // Page size
}
```

### 2. **File Upload Management**
```java
// Backend file handling
private static final String IMAGE_DIRECTORY = System.getProperty("user.dir") + "/product-image/";
private static final String IMAGE_DIRECTOR_FRONTEND = "D:\\GitHub\\InventoryManagement\\frontend\\public\\images\\";

// Unique filename generation
String filename = System.currentTimeMillis() + "_" + 
                 (int)(Math.random() * 10000) + "_" + 
                 originalFilename.replaceAll("\\s+", "_").toLowerCase();
```

### 3. **Advanced Pagination**
```typescript
// Client-side pagination logic
totalPages = Math.ceil(allProducts.length / itemsPerPage);
products = allProducts.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
);
```

### 4. **Database Query Complexity**
```sql
-- Search query với JOIN và multiple conditions
SELECT p.* FROM products p
LEFT JOIN categories c ON p.category_id = c.id
WHERE c.name = ? AND (
    ? IS NULL OR
    LOWER(p.name) LIKE LOWER(CONCAT('%', ?, '%')) OR
    LOWER(p.sku) LIKE LOWER(CONCAT('%', ?, '%')) OR
    CAST(p.stock_quantity AS string) = ?
) ORDER BY p.id DESC
```

### 5. **Form State Management**
```typescript
interface ProductForm {
    isEditing: boolean;
    productId: string | null;
    name: string;
    sku: string;               // Unique constraint
    price: string;             // Must be positive
    stockQuantity: string;     // Must be >= 0
    categoryId: string;        // Foreign key
    description?: string;
    imageFile?: File;
    imageUrl?: string;
}
```

### 6. **Role-based Access Control**
```html
<!-- Admin-only buttons -->
<button *ngIf="isAdmin()" class="btn btn-app" (click)="navigateToAddProductPage()">
<td *ngIf="isAdmin()"><button (click)="navigateToEditProductPage(product.id)">Edit</button></td>
<td *ngIf="isAdmin()"><button (click)="handleProductDelete(product.id)">Delete</button></td>
```

### 7. **Navigation Patterns**
- **List View**: `/product` - với optional `?category=categoryName`
- **Add View**: `/add-product` - Form thêm mới
- **Edit View**: `/edit-product/{id}` - Form edit với pre-filled data
- **Search Mode**: Same page với filtered results

### 8. **Data Validation Layers**
```java
// Entity validation
@NotBlank(message = "Name is required")
private String name;

@NotBlank(message = "Sku is required")
@Column(unique = true)
private String sku;

@Positive(message = "Product price must be a positive value")
private BigDecimal price;

@Min(value = 0, message = "Stock quantity cannot be lesser than zero")
private Integer stockQuantity;
```

### 9. **Error Handling Strategies**
- **Network Errors**: Retry mechanisms với exponential backoff
- **Validation Errors**: Client + server-side validation
- **File Upload Errors**: Type, size, permission checking
- **Concurrent Access**: Optimistic locking protection
- **Foreign Key Violations**: Graceful constraint error handling

### 10. **Performance Optimizations**
- **Client-side Pagination**: Reduce server load
- **Image Optimization**: File size limits và compression
- **Search Debouncing**: Prevent excessive API calls
- **Lazy Loading**: Load categories only when needed
- **Caching**: Store frequent queries results

### 11. **Business Rules**
1. **ADMIN Authorization**: Chỉ ADMIN mới có quyền CRUD
2. **SKU Uniqueness**: Mỗi product phải có SKU unique
3. **Category Dependency**: Product phải thuộc về 1 category hợp lệ
4. **Stock Management**: Stock quantity không được âm
5. **Price Validation**: Price phải positive
6. **Image Management**: Support multiple image formats với size limits
7. **Search Flexibility**: Search by name, SKU, hoặc exact stock quantity
8. **Pagination**: Handle large datasets efficiently
9. **Confirmation**: Delete operations cần user confirmation
10. **Foreign Key Protection**: Không thể delete product đang được sử dụng

Hệ thống Product Management này rất comprehensive với full CRUD operations, advanced search, file upload, pagination, và robust error handling phù hợp cho enterprise-level inventory management system.